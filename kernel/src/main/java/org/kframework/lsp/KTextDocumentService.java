package org.kframework.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jetbrains.annotations.NotNull;
import org.kframework.attributes.Att;
import org.kframework.attributes.Location;
import org.kframework.kil.*;
import org.kframework.kil.Module;
import org.kframework.kompile.Kompile;
import org.kframework.kore.Sort;
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
import static org.kframework.Collections.*;

/**
 * TextDocumentService implementation for K.
 */
public class KTextDocumentService implements TextDocumentService {

    private final KLanguageServer languageServer;
    private final LSClientLogger clientLogger;

    public final TextDocumentSyncHandler memo;
    public static final URI domains;
    public static final URI kast;
    // time delay after which to start doing completion calculation
    public static long DELAY_EXECUTION_MS = 1000;

    static {
        try {
            domains = Path.of(Kompile.BUILTIN_DIRECTORY.toString(), "domains.md").toRealPath(LinkOption.NOFOLLOW_LINKS).toUri();
            kast = Path.of(Kompile.BUILTIN_DIRECTORY.toString(), "kast.md").toRealPath(LinkOption.NOFOLLOW_LINKS).toUri();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public KTextDocumentService(KLanguageServer languageServer) throws URISyntaxException {
        this.languageServer = languageServer;
        this.clientLogger = LSClientLogger.getInstance();
        memo = new TextDocumentSyncHandler(clientLogger);
        memo.add(domains.toString());
        memo.add(kast.toString());
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {
        this.clientLogger.logMessage("Operation '" + "text/didOpen" +
                "' {fileUri: '" + didOpenTextDocumentParams.getTextDocument().getUri() + "'} opened");
        memo.didOpen(didOpenTextDocumentParams);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams didChangeTextDocumentParams) {
        this.clientLogger.logMessage("Operation '" + "text/didChange" +
                "' {fileUri: '" + didChangeTextDocumentParams.getTextDocument().getUri() + "'} Changed");
        memo.didChange(didChangeTextDocumentParams);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {
        this.clientLogger.logMessage("Operation '" + "text/didClose" +
                "' {fileUri: '" + didCloseTextDocumentParams.getTextDocument().getUri() + "'} Closed");
        if (!(didCloseTextDocumentParams.getTextDocument().getUri().equals(domains.toString())
                || didCloseTextDocumentParams.getTextDocument().getUri().equals(kast.toString())))
            memo.didClose(didCloseTextDocumentParams);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {
        this.clientLogger.logMessage("Operation '" + "text/didSave" +
                "' {fileUri: '" + didSaveTextDocumentParams.getTextDocument().getUri() + "'} Saved");
        memo.didSave(didSaveTextDocumentParams);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return memo.completion(position);
    }

    @Override
    public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
        return memo.diagnostic(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends org.eclipse.lsp4j.Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        return memo.definition(params);
    }
}