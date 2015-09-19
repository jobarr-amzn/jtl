package org.dykman.jtl.future;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

abstract class ObjectInstructionBase extends AbstractInstructionFuture {
      final List<Pair<String, InstructionFuture<JSON>>> ll;

      public ObjectInstructionBase(SourceInfo info, List<Pair<String, InstructionFuture<JSON>>> pp, boolean itemize) {
         super(info, true);
         ll = pp;
      }

      public abstract ListenableFuture<JSON> _callObject(final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException;

      public final ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException {
         if(context.isInclude()) {
            return _import(context, data);
         } else {
            return _callObject(context, data);
         }

      }

      public ListenableFuture<JSON> _import(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data) {
         List<InstructionFuture<JSON>> imperitives = new ArrayList<>(ll.size());
         List<ListenableFuture<JSON>> rr = new ArrayList<>();

         // ListenableFuture<JSON> initInst = null;
         InstructionFuture<JSON> init = null;
         for(Pair<String, InstructionFuture<JSON>> pp : ll) {
            String k = pp.f;
            InstructionFuture<JSON> inst = pp.s;
            if(k.equals("!init")) {
               init = InstructionFutureFactory.singleton(inst.getSourceInfo(), inst);
               /*
                * if(init == null) synchronized(this) { if(init == null) { init
                * = singleton(inst.getSourceInfo(), inst); } }
                */
            } else if(k.equals("_")) {
               // ignore in import
            } else if(k.startsWith("!")) {
               // variable, (almost) immediate evaluation
               InstructionFuture<JSON> imp = inst;
               context.define(k.substring(1), imp);
               imperitives.add(imp);
            } else if(k.startsWith("$")) {
               // variable, deferred evaluation
               context.define(k.substring(1), InstructionFutureFactory.deferred(inst.getSourceInfo(), inst, context, data));
            } else {
               // define a function
               context.define(k, InstructionFutureFactory.fixContextData(inst.getSourceInfo(), inst));
            }

         }
         return null;
      }

      public List<Pair<String, InstructionFuture<JSON>>> pairs() {
         return ll;
      };
   }