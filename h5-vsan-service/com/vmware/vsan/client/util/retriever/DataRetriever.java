package com.vmware.vsan.client.util.retriever;

import java.util.concurrent.ExecutionException;

interface DataRetriever {
   void start();

   Object getResult() throws ExecutionException, InterruptedException;
}
