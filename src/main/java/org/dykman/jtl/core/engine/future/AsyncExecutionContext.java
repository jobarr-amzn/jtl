package org.dykman.jtl.core.engine.future;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public interface AsyncExecutionContext<T> {
	public void define(String n,InstructionFuture<T> i);
	
	public ListeningExecutorService executor();
	
	public ListenableFuture<JSON> execute(InstructionFuture<JSON> inst,
			ListenableFuture<JSON> data);

	public InstructionFuture<T> getdef(String name);

	public void set(String name,ListenableFuture<T> t);
	public ListenableFuture<T> lookup(String name,ListenableFuture<T> t)
		throws ExecutionException;
	
	public AsyncExecutionContext<T> getNamedContext(String label);
	public AsyncExecutionContext<T> getNamedContext(String label,boolean create);
	
	public AsyncExecutionContext<T> createChild(boolean fc);
}
