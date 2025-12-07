User send messages
    -LLM process request
    -LLM generates plan structure
    -ReAct starts 
    -System adds more in history
    -History is saved by markers
        -ex:
            -User send request
                -request saved
            -LLM process first step
                -step saved
            -Backend execute function
                -backend response saved
            -LLM process second step
                -step saved
            -Backend execute function
                -backend response saved
            -LLM sees that request is fullfilled
            -LLM sends final answer
                -final answer saved
        -through this process, if history
        exceed length then we cut it, summarize the past history and replace the current history by summary and history id (so that the LLM can retrieve history if needed)


My implementation idea suggests that we never remove old history but summarize then
reducing history length but at some point the history will be too long to be processed by LLM as the summarized part of the history will grow exponentially as the conversation gets longer (if you understand what i'm saying here explain through a mathematical formula)

So i suggest introducting soft cut and hard cut. Soft cut is what we want to do:
    -never remove old history but summarize it
But then we will store frequence of cut, when the frequence is low, that means that we can still keep the old history as summarized but when is gets bigger which means we reach the point where we get closer and closer to the maximum size for each request then we switch to hard cut and remove completely half of the old history and then we start again with soft cut
