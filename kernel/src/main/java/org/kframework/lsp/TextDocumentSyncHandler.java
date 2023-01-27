package org.kframework.lsp;


import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.kframework.attributes.Att;
import org.kframework.kil.*;
import org.kframework.kil.Module;
import org.kframework.kore.Sort;
import org.kframework.utils.file.FileUtil;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.kframework.Collections.immutable;

/**
 * Handle the caches of all the files of interest.
 */
public class TextDocumentSyncHandler {

    public Map<String, KTextDocument> files = new HashMap<>();
    private final LSClientLogger clientLogger;

    public TextDocumentSyncHandler(LSClientLogger clientLogger) {
        this.clientLogger = clientLogger;
    }

    public void add(String uri) {
        try {
            KTextDocument doc = new KTextDocument();
            files.put(uri, doc);
            doc.uri = uri;
            doc.updateText(FileUtil.load(new File(new URI(uri))));
            doc.outerParse();
        } catch (URISyntaxException e) {
            clientLogger.logMessage(TextDocumentSyncHandler.class + ".addUri failed: " + uri);
        }
    }

    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        KTextDocument doc;
        if (files.containsKey(uri))
            doc = files.get(uri);
        else {
            doc = new KTextDocument();
            doc.uri = uri;
            files.put(uri, doc);
        }
        doc.updateText(params.getTextDocument().getText());
    }

    public void didChange(DidChangeTextDocumentParams params) {
        files.get(params.getTextDocument().getUri()).updateText(params.getContentChanges().get(0).getText());
    }

    public void didSave(DidSaveTextDocumentParams params) {
        KTextDocument doc = files.get(params.getTextDocument().getUri());
        doc.outerParse();
    }

    public void didClose(DidCloseTextDocumentParams params) {
        files.remove(params.getTextDocument().getUri());
    }

    // recurse through all the required files and return the list of DefinitionItems
    public List<DefinitionItem> slurp(String uri) {
        Set<String> visited = new HashSet<>();
        visited.add(KTextDocumentService.domains.toString());
        visited.add(KTextDocumentService.kast.toString());
        slurp2(uri, visited);
        List<DefinitionItem> dis = files.entrySet().stream()
                .filter(e -> visited.contains(e.getKey()))
                .flatMap(e -> e.getValue().dis.stream())
                .collect(Collectors.toList());
        return dis;
    }

    private void slurp2(String uri, Set<String> visited) {
        if (!files.containsKey(uri)) {
            try {
                if (new File(new URI(uri)).exists())
                    add(uri);
            } catch (URISyntaxException e) {
                clientLogger.logMessage(TextDocumentSyncHandler.class + ".slurp failed1: " + uri + "\n" + e.getMessage());
            }
        }
        KTextDocument current = files.get(uri);
        if (current.parsingOutdated)
            current.outerParse();
        if (!visited.contains(uri) && files.containsKey(uri)) {
            visited.add(uri);
            files.get(uri).dis.forEach(r -> {
                if (r instanceof Require) {
                    try {
                        Require req = (Require) r;
                        URI targetURI = Path.of(new File(new URI(uri)).getParent(), req.getValue()).toRealPath(LinkOption.NOFOLLOW_LINKS).toUri();
                        slurp2(targetURI.toString(), visited);
                    } catch (IOException | URISyntaxException e) {
                        clientLogger.logMessage(TextDocumentSyncHandler.class + ".slurp failed2: " + uri + "\n" + e);
                    }
                }
            });
        }
    }

    // Create a list of CompletionItems depending on the context.
    // A work in progress file may not parse, but we still want to have completion working, therefore we use a regex
    // to find the closest keyword, and approximate the context.
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return CompletableFuture.supplyAsync(() -> {
            KPos pos = new KPos(position.getPosition());
            List<CompletionItem> lci = new ArrayList<>();
            KTextDocument doc = files.get(position.getTextDocument().getUri());
            String context = doc.getContextAt(pos);
            List<DefinitionItem> allDi = slurp(position.getTextDocument().getUri());
            switch (context) {
                case "import":
                case "imports":
                    lci.addAll(getImportCompletion(allDi)); break;
                case "syntax":
                    lci.addAll(getSyntaxCompletion(allDi)); break;
                case "context":
                case "rule":
                case "configuration":
                case "claim":
                    lci.addAll(getRuleCompletion(allDi)); break;
            }
            this.clientLogger.logMessage("Operation '" + "text/completion: " + position.getTextDocument().getUri() + " #pos: "
                    + pos.getLine() + " " + pos.getCharacter() + " context: " + context + " #: " + lci.size());

            // TODO: add completion for attributes
            return Either.forLeft(lci);
        });
    }

    private static List<CompletionItem> getImportCompletion(List<DefinitionItem> allDi) {
        return allDi.stream()
                .filter(mi2 -> mi2 instanceof Module)
                .map(m2 -> ((Module) m2))
                .map(m2 -> {
                    CompletionItem ci = new CompletionItem();
                    ci.setLabel(m2.getName());
                    ci.setInsertText(m2.getName());
                    ci.setDetail(Path.of(m2.getSource().source()).getFileName().toString());
                    ci.setKind(CompletionItemKind.Snippet);
                    return ci;
                }).collect(Collectors.toList());
    }

    // create a list of all the visible declared sorts
    private static List<CompletionItem> getSyntaxCompletion(List<DefinitionItem> allDi) {
        Map<String, Set<Att>> allSorts = allDi.stream().filter(i -> i instanceof Module)
                .map(m3 -> ((Module) m3))
                .flatMap(m3 -> m3.getItems().stream()
                        .filter(mi3 -> mi3 instanceof Syntax)
                        .map(s -> ((Syntax) s))
                        .filter(s -> !s.getParams().contains(s.getDeclaredSort().getRealSort()))
                        .map(s -> Tuple2.apply(s.getDeclaredSort().getRealSort().name(), s.getAttributes())))
                .collect(Collectors.groupingBy(Tuple2::_1, Collectors.mapping(Tuple2::_2, Collectors.toSet())));
        Map<String, Att> allSorts2 = allSorts.entrySet().stream()
                .map(e -> Tuple2.apply(e.getKey(), Att.mergeAttributes(immutable(e.getValue()))))
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
        return allSorts2.entrySet().stream().map(e -> {
            CompletionItem ci = new CompletionItem();
            ci.setLabel(e.getKey());
            ci.setInsertText(e.getKey());
            // TODO: to calculate properly we need to recurse through the inclusion tree
            // and find the first module to declare the sort. This should be easy to get
            // when we connect to the kompile pipeline.
            //ci.setDetail("module " + m.getName());
            String documentation = "syntax " + e.getKey() + " ";
            documentation += e.getValue().toString();
            ci.setDocumentation(documentation);
            ci.setKind(CompletionItemKind.Snippet);
            return ci;
        }).collect(Collectors.toList());
    }

    static Pattern ptrn = Pattern.compile("[a-zA-Z0-9#]+");

    // create the list of CompletionItems for every piece of syntax
    // for now we filter by alphanumeric but can be improved greatly.
    private static List<CompletionItem> getRuleCompletion(List<DefinitionItem> dis) {
        List<CompletionItem> lci = new ArrayList<>();
        // Traverse all the modules and all the syntax declarations to find the Terminals in productions
        // For each Terminal that follows the <ptrn> above, create a CompletionItem with some documentation
        // Tree structure: Definition -> Module -> Syntax -> PriorityBlock -> Production -> Terminal
        dis.stream().filter(i -> i instanceof Module)
                .map(m -> ((Module) m))
                .forEach(m -> m.getItems().stream()
                        .filter(mi -> mi instanceof Syntax)
                        .map(s -> ((Syntax) s))
                        .forEach(s -> s.getPriorityBlocks()
                                .forEach((pb -> pb.getProductions()
                                        .forEach(p -> p.getItems().stream()
                                                .filter(pi -> pi instanceof Terminal)
                                                .map(t -> (Terminal) t)
                                                .forEach(t -> {
                                                    if (ptrn.matcher(t.getTerminal()).matches()) {
                                                        CompletionItem completionItem = buildRuleCompletionItem(m, s, p, t);
                                                        lci.add(completionItem);
                                                    }
                                                }))))));

        return lci;
    }

    @NotNull
    private static CompletionItem buildRuleCompletionItem(Module m, Syntax s, Production p, Terminal t) {
        CompletionItem completionItem = new CompletionItem();
        completionItem.setLabel(t.getTerminal());
        completionItem.setInsertText(t.getTerminal());
        completionItem.setDetail("module " + m.getName());
        String doc = "syntax ";
        doc += !s.getParams().isEmpty() ?
                "{" + s.getParams().stream().map(Sort::toString).collect(Collectors.joining(", ")) + "} " : "";
        doc += s.getDeclaredSort() + " ::= ";
        doc += p.toString();
        completionItem.setDocumentation(doc);
        completionItem.setKind(CompletionItemKind.Snippet);
        return completionItem;
    }

    // At the moment we only have access to outer parsing information, so we can find the definition for
    // imported module names and required files
    public CompletableFuture<Either<List<? extends org.eclipse.lsp4j.Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        KPos pos = new KPos(params.getPosition());
        return CompletableFuture.supplyAsync(() -> {
            this.clientLogger.logMessage("Operation '" + "text/definition: " + params.getTextDocument().getUri() + " #pos: " + pos.getLine() + " " + pos.getCharacter());
            List<LocationLink> lls = new ArrayList<>();
            try {
                List<DefinitionItem> dis = files.get(params.getTextDocument().getUri()).dis;
                for (DefinitionItem di : dis) {
                    if (di instanceof Require) {
                        org.kframework.attributes.Location loc = getSafeLoc(di);
                        if (isPositionOverLocation(pos, loc)) {
                            Require req = (Require) di;
                            File f = new File(new URI(params.getTextDocument().getUri()));
                            URI targetURI = Path.of(f.getParent(), req.getValue()).toRealPath(LinkOption.NOFOLLOW_LINKS).toUri();
                            lls.add(new LocationLink(targetURI.toString(),
                                    new Range(new Position(0, 0), new Position(10, 0)),
                                    new Range(new Position(0, 0), new Position(0, 0)),
                                    loc2range(loc)));
                            break;
                        }
                    } else if (di instanceof Module) {
                        Module m = (Module) di;
                        for (ModuleItem mi : m.getItems()) {
                            if (mi instanceof Import) {
                                org.kframework.attributes.Location loc = getSafeLoc(mi);
                                if (isPositionOverLocation(pos, loc)) {
                                    Import imp = (Import) mi;
                                    List<DefinitionItem> allDi = slurp(params.getTextDocument().getUri());
                                    allDi.stream().filter(ddi -> ddi instanceof Module)
                                            .map(ddi -> ((Module) ddi))
                                            .filter(m2 -> m2.getName().equals(imp.getName()))
                                            .forEach(m3 -> lls.add(new LocationLink(URI.create(m3.getSource().source()).toString(),
                                                    loc2range(getSafeLoc(m3)),
                                                    loc2range(getSafeLoc(m3)),
                                                    loc2range(getSafeLoc(imp)))));
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (URISyntaxException | IOException e) {
                clientLogger.logMessage("definition failed: " + params.getTextDocument().getUri() + e);
            }

            return Either.forRight(lls);
        });
    }

    // in case a node doesn't have a location information, return Location(0,0,0,0)
    public static org.kframework.attributes.Location getSafeLoc(ASTNode node) {
        return node.location().orElse(new org.kframework.attributes.Location(0,0,0,0));
    }

    // true if Position(l,c) is over Location(startL, startC, endL, endC). Expects Position to start from 1, just as Location
    public static boolean isPositionOverLocation(KPos pos, org.kframework.attributes.Location loc) {
        return (loc.startLine() < pos.getLine() || (loc.startLine() == pos.getLine() && loc.startColumn() <= pos.getCharacter())) &&
                (pos.getLine() < loc.endLine() || (pos.getLine() == loc.endLine() && pos.getCharacter() <= loc.endColumn()));
    }

    // K starts counting lines and columns from 1 and LSP starts counting from 0
    public static Range loc2range(org.kframework.attributes.Location loc) {
        return new Range(new Position(loc.startLine() - 1 , loc.startColumn() - 1), new Position(loc.endLine() - 1, loc.endColumn() - 1));
    }

    // previous diagnostics task. If it's still active, cancel it and run a newer, updated one
    private CompletableFuture<DocumentDiagnosticReport> latestDiagnosticScheduled;

    public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
        if (latestDiagnosticScheduled != null && !latestDiagnosticScheduled.isDone())
            latestDiagnosticScheduled.completeExceptionally(new Throwable("Cancelled diagnostic publisher"));

        Executor delayedExecutor = CompletableFuture.delayedExecutor(KTextDocumentService.DELAY_EXECUTION_MS, TimeUnit.MILLISECONDS);
        CompletableFuture<DocumentDiagnosticReport> scheduledFuture = CompletableFuture.supplyAsync(() -> {
            files.get(params.getTextDocument().getUri()).outerParse();
            List<Diagnostic> problems = files.get(params.getTextDocument().getUri()).problems;
            DocumentDiagnosticReport report = new DocumentDiagnosticReport(new RelatedFullDocumentDiagnosticReport(problems));
            this.clientLogger.logMessage("Operation '" + "text/diagnostics: " + params.getTextDocument().getUri() + " #problems: " + problems.size());
            return report;
        }, delayedExecutor);
        latestDiagnosticScheduled = scheduledFuture;
        return scheduledFuture;
    }
}