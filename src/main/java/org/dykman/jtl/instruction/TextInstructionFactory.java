package org.dykman.jtl.instruction;

import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONValue;
import org.dykman.jtl.operator.FutureInstruction;

public abstract class TextInstructionFactory extends ContextualInstructionFactory {

	public static FutureInstruction<JSON> sprintf() {
		SourceInfo info = SourceInfo.internal("ContextualInstructionFactory::sprintf");
		return ContextualInstructionFactory.contextualVarArgInstruction(info, (context, args) -> {
			int length = args.length;
			if (length > 0) {
				String format = args[0].stringValue();
				Object[] objs = new Object[length - 1];
				for (int i = 1; i < length; ++i) {
					JSON j = args[i];
					switch (j.getType()) {
					case BOOLEAN:
						objs[i - 1] = ((JSONValue) j).booleanValue();
						break;
					case LONG:
						objs[i - 1] = ((JSONValue) j).longValue();
						break;
					case DOUBLE:
						objs[i - 1] = ((JSONValue) j).doubleValue();
						break;
					case STRING:
						objs[i - 1] = ((JSONValue) j).stringValue();
						break;
					case OBJECT:
					case ARRAY:
					case LIST:
						objs[i - 1] = "(illegal value)";
						break;
					case NULL:
						objs[i - 1] = "(null)";
						break;
					}
				}
				return context.builder().value(String.format(format, objs));
	
			}
			return context.builder().value("");
		});
	
	}

}
