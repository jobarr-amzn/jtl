package org.dykman.jtl;

import static org.dykman.jtl.operator.FutureInstructionFactory.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.dykman.jtl.jtlParser.JtlContext;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.FutureInstructionValue;
import org.dykman.jtl.future.FutureInstructionVisitor;
import org.dykman.jtl.future.SimpleExecutionContext;
import org.dykman.jtl.instruction.HttpInstructionFactory;
import org.dykman.jtl.instruction.TextInstructionFactory;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.operator.FutureInstruction;
import org.dykman.jtl.operator.FutureInstructionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class JtlCompiler {
	
	static Logger logger = LoggerFactory.getLogger(JtlCompiler.class);

	final JSONBuilder jsonBuilder;
	boolean trace;
	boolean profile;
	boolean imported;
	boolean syntaxCheck = false;
	
	public boolean isSyntaxCheck() {
		return syntaxCheck;
	}

	public void setSyntaxCheck(boolean syntaxCheck) {
		this.syntaxCheck = syntaxCheck;
	}

	public JtlCompiler(JSONBuilder jsonBuilder) {
		this(jsonBuilder, false, false, false);
	}

	private JtlCompiler(JSONBuilder jsonBuilder, boolean trace, boolean profile, boolean imported) {
		this.jsonBuilder = jsonBuilder;
		this.trace = trace;
		this.profile = profile;
		this.imported = imported;
	}

	public FutureInstruction<JSON> parse(String src, InputStream in) throws IOException {
		return parse(src, in, trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, InputStream in, boolean trace, boolean profile)
			throws IOException {
		return parse(src, new jtlLexer(new ANTLRInputStream(in)), trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, File in, boolean trace, boolean profile) throws IOException {
		return parse(src, new FileInputStream(in), trace, profile);
	}

	public FutureInstruction<JSON> parse(File in) throws IOException {
		// System.err.println("parsing file: " + in.getAbsolutePath());
		return parse(in.getName(), in, trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, String in) throws IOException {
		return parse(src, in, trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, String in, boolean trace, boolean profile) throws IOException {
		return parse(src, new jtlLexer(new ANTLRInputStream(in)), trace, profile);
	}

	protected FutureInstruction<JSON> parse(String src, jtlLexer lexer) throws IOException {
		return parse(src, lexer, trace, profile);
	}

	protected FutureInstruction<JSON> parse(
			String file, jtlLexer lexer, boolean trace, boolean profile)
			throws IOException {
		jtlParser parser = new jtlParser(new CommonTokenStream(lexer));

		parser.addErrorListener(new ANTLRErrorListener() {
			
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
					String msg, RecognitionException e) {
				StringBuilder builder = new StringBuilder();
				builder.append(msg).append(" `").append(offendingSymbol.toString())
					.append("' at line ").append(line).append(": ").append(charPositionInLine);

				logger.error(builder.toString());
				System.exit(1);
			}
			
			@Override
			public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction,
					ATNConfigSet configs) {
				// TODO Auto-generated method stub
				
				if(logger.isDebugEnabled()) logger.debug("ContextSensitivity");
			}
			
			@Override
			public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
					BitSet conflictingAlts, ATNConfigSet configs) {
				// TODO Auto-generated method stub
				
				if(logger.isDebugEnabled()) logger.debug("AttemptingFullContext");
			}
			
			@Override
			public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
					BitSet ambigAlts, ATNConfigSet configs) {
				// TODO Auto-generated method stub
				if(logger.isDebugEnabled()) logger.debug("ambiguity detected");
				
			}
		});
		parser.setTrace(trace);
		parser.setProfile(profile);
		try {
			JtlContext tree = parser.jtl();
//			RecognitionException re = tree.exception;
			FutureInstructionVisitor visitor = 
					new FutureInstructionVisitor(file, jsonBuilder, imported);
			FutureInstructionValue<JSON> v = visitor.visit(tree);
			if(syntaxCheck && v != null) {
				return FutureInstructionFactory.value("syntax ok", jsonBuilder, SourceInfo.internal("syntax check"));
			} else {
				return v.inst;
//				return FutureInstructionFactory.fixContextData(v.inst);
			}
		} catch(JtlParseException e) {
			logger.error(e.report(),e);
			return FutureInstructionFactory.value(jsonBuilder.value(), 
					SourceInfo.internal("parser"));
		} catch(Exception e) {
			logger.info(e.getLocalizedMessage(),e);
			return FutureInstructionFactory.value(jsonBuilder.value(), 
					SourceInfo.internal("parser"));
		}
	}

	public static SourceInfo getSource(String source, ParserRuleContext ctx, String name) {
		SourceInfo info = new SourceInfo();
		info.line = ctx.start.getLine();
		info.position = ctx.start.getCharPositionInLine();
		info.endline = ctx.stop.getLine();
		info.endposition = ctx.stop.getCharPositionInLine() + ctx.stop.getText().length();
		info.code = ctx.getText();
		info.source = source == null ? ctx.start.getTokenSource().getSourceName() : source;
		info.name = name;
		return info;
	}

	public JSONBuilder getJsonBuilder() {
		return jsonBuilder;
	}
/*
	public static void define(AsyncExecutionContext<JSON> ctx, String s, FutureInstruction<JSON> inst) {
		ctx.define(s, inst);
	}
*/
	
	public AsyncExecutionContext<JSON> createInitialContext(
			JSON data, JSONObject config, File scriptBase, File init,
			JSONBuilder builder, ListeningExecutorService les) throws IOException, ExecutionException {

		ListenableFuture<JSON> df = Futures.immediateCheckedFuture(data);
		SimpleExecutionContext context = new SimpleExecutionContext(builder,null, df, config, scriptBase,
				SourceInfo.internal("base-init"));
		context.setExecutionService(les);

		// configurable: import, extend
		context.define("_", FutureInstructionFactory.value(df, SourceInfo.internal("input")));

		context.define( "module", loadModule(SourceInfo.internal("module"), config));
		context.define( "import", importInstruction(SourceInfo.internal("import"), config));

		
		context.define( "try", attempt(SourceInfo.internal("try")));

		// general
		context.define( "error", defaultError(SourceInfo.internal("error")));
		context.define( "params", params(SourceInfo.internal("params")));
		context.define( "rand", rand(SourceInfo.internal("rand")));
		context.define( "switch", switchInst(SourceInfo.internal("switch")));
		context.define( "each", each(SourceInfo.internal("each"),true));
		context.define( "reduce", reduce(SourceInfo.internal("reduce"),true));
		
		context.define("defined",defined(SourceInfo.internal("defined")));
		context.define( "call", call(SourceInfo.internal("call")));
		context.define( "thread", thread(SourceInfo.internal("thread")));
		context.define( "type", type(SourceInfo.internal("type")));
		context.define( "json", parseJson(SourceInfo.internal("json")));
		
		// debugging, stderr
		context.define( "trace", trace(SourceInfo.internal("trace")));
		context.define( "stack", stack(SourceInfo.internal("stack")));
		context.define( "log", log(SourceInfo.internal("log")));

		// misc.
		context.define( "counter", stack(SourceInfo.internal("counter")));
		context.define( "digest", digest(SourceInfo.internal("digest")));

		// external data
		context.define( "env", env(SourceInfo.internal("env")));
		context.define( "cli", cli(SourceInfo.internal("cli")));
		context.define( "exec", exec(SourceInfo.internal("exec")));
		context.define( "file", file(SourceInfo.internal("file")));
		context.define( "url", url(SourceInfo.internal("url")));
		context.define( "write", write(SourceInfo.internal("write")));
		context.define( "mkdirs", mkdirs(SourceInfo.internal("mkdirs")));
		context.define( "fexists", fexists(SourceInfo.internal("fexists")));
		
		// http-specific
		context.define("header",HttpInstructionFactory.responseHeader());
		context.define("status",HttpInstructionFactory.httpStatus());

		// string-oriented
		context.define( "split", split(SourceInfo.internal("split")));
		context.define( "join", join(SourceInfo.internal("join")));
		context.define( "substr", substr(SourceInfo.internal("substr")));
		context.define( "sprintf",TextInstructionFactory.sprintf());
		context.define( "upper", tocase(SourceInfo.internal("upper"),true));
		context.define( "lower", tocase(SourceInfo.internal("lower"),false));
		

		// list-oriented
		context.define( "unique", unique(SourceInfo.internal("unique")));
		context.define( "count", count(SourceInfo.internal("count")));
		context.define( "sort", sort(SourceInfo.internal("sort"), false));
		context.define( "rsort", sort(SourceInfo.internal("rsort"), true));
		context.define( "filter", filter(SourceInfo.internal("filter")));
		context.define( "contains", contains(SourceInfo.internal("contains")));
		context.define( "copy", copy(SourceInfo.internal("copy")));
		context.define( "append", append(SourceInfo.internal("append")));
		context.define( "sum", sum(SourceInfo.internal("sum")));
		context.define( "min", min(SourceInfo.internal("min")));
		context.define( "max", max(SourceInfo.internal("max")));
		context.define( "avg", avg(SourceInfo.internal("avg")));
		context.define( "pivot", pivot(SourceInfo.internal("pivot")));

		// object-oriented
		context.define( "group", groupBy(SourceInfo.internal("group")));
		context.define( "map", map(SourceInfo.internal("map")));
		context.define( "collate", collate(SourceInfo.internal("collate")));
		context.define( "omap", omap(SourceInfo.internal("omap")));
		context.define( "amend", amend(SourceInfo.internal("amend")));
		context.define( "keys", keys(SourceInfo.internal("keys")));
		context.define( "rekey", rekey(SourceInfo.internal("rekey")));


		// 0 args: return boolean type test
		// 1 arg: attempts to coerce to the specified type
		context.define( "array", isArray(SourceInfo.internal("array")));
		context.define( "number", isNumber(SourceInfo.internal("number")));
		context.define( "int", isInt(SourceInfo.internal("int")));
		context.define( "real", isReal(SourceInfo.internal("real")));
		context.define( "string", isString(SourceInfo.internal("string")));
		context.define( "boolean", isBoolean(SourceInfo.internal("boolean")));

		// 0 args: boolean type test only
		context.define( "nil", isNull(SourceInfo.internal("nil")));
		context.define( "value", isValue(SourceInfo.internal("value")));
		context.define( "object", isObject(SourceInfo.internal("object")));

		AsyncExecutionContext<JSON> ctx = context;
		if (init != null) {
			ctx = ctx.createChild(false, false, df, SourceInfo.internal("user-init"));
			ctx.setInit(true);
			FutureInstruction<JSON> initf = parse(init);
			ListenableFuture<JSON> initResult = initf.call(ctx, Futures.immediateCheckedFuture(config));
			ctx.define("init", FutureInstructionFactory.value(initResult, SourceInfo.internal("init")));
		}
		ctx.setInit(true);
		return ctx;
	}
}
