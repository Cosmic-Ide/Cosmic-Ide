/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;

import com.google.common.collect.ArrayListMultimap;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Pair;

import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * @author Tomas Zezula
 */
public class NBLog extends Log {

    private static final String ERR_NOT_IN_PROFILE = "not.in.profile";  //NOI18N

    private final Map<URI, Collection<Symbol.ClassSymbol>> notInProfiles =
            new HashMap<>();
    private final Set<Integer> seenPartialReparsePositions = new HashSet<>();
    private final ArrayListMultimap<URI, JCDiagnostic> diagnosticMap = ArrayListMultimap.create();
    private JavaFileObject partialReparseFile;


    protected NBLog(
            final Context context,
            final PrintWriter output) {
        super(context, output);
    }

    public static NBLog instance(Context context) {
        final Log log = Log.instance(context);
        if (!(log instanceof NBLog)) {
            throw new InternalError("No NBLog instance!"); //NOI18N
        }
        return (NBLog) log;
    }

    public static void preRegister(Context context,
                                   final PrintWriter errWriter,
                                   final PrintWriter warnWriter,
                                   final PrintWriter noticeWriter) {
        context.put(logKey, (Context.Factory<Log>) c -> new NBLog(
                c,
                errWriter));
    }

    public static void preRegister(Context context,
                                   final PrintWriter output) {
        context.put(logKey, (Context.Factory<Log>) c -> new NBLog(
                c,
                output));
    }

    @Override
    public DiagnosticSource getSource(JavaFileObject file) {
        return super.getSource(file);
    }

    public void removeFileObject(JavaFileObject fileObject) {
        sourceMap.remove(fileObject);
    }

    @Override
    public void report(JCDiagnostic diagnostic) {
        diagnosticMap.put(diagnostic.getSource().toUri(), diagnostic);

        //XXX: needs testing!
        if (diagnostic.getKind() == Diagnostic.Kind.ERROR &&
                ERR_NOT_IN_PROFILE.equals(diagnostic.getCode())) {
            final JavaFileObject currentFile = currentSourceFile();
            if (currentFile != null) {
                final URI uri = currentFile.toUri();
                Symbol.ClassSymbol type = (Symbol.ClassSymbol) diagnostic.getArgs()[0];
                Collection<Symbol.ClassSymbol> types =
                        notInProfiles.computeIfAbsent(uri, k -> new ArrayList<>());
                types.add(type);
            }
        }
        super.report(diagnostic);
    }

    public List<JCDiagnostic> getDiagnostics(URI uri) {
        return diagnosticMap.get(uri);
    }

    @Override
    protected boolean shouldReport(JavaFileObject file, int pos) {
        if (true) {
            return false;
        }
        if (partialReparseFile != null) {
            return file.toUri().equals(partialReparseFile.toUri()) && seenPartialReparsePositions.add(pos);
        } else {
            return super.shouldReport(file, pos);
        }
    }

    Collection<? extends Symbol.ClassSymbol> removeNotInProfile(final URI uri) {
        return uri == null ? null : notInProfiles.remove(uri);
    }

    @Override
    protected int getDefaultMaxWarnings() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected int getDefaultMaxErrors() {
        return Integer.MAX_VALUE;
    }

    public void startPartialReparse(JavaFileObject inFile) {
        partialReparseFile = inFile;
    }

    public void endPartialReparse(JavaFileObject inFile) {
        partialReparseFile = null;
        seenPartialReparsePositions.clear(); //TODO: not tested
    }

    public Set<Pair<JavaFileObject, Integer>> getRecorded() {
        return recorded;
    }

    public void removeDiagnostics(URI toUri) {
        diagnosticMap.removeAll(toUri);
    }
}