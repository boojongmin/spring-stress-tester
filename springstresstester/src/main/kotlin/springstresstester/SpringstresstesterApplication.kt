package springstresstester

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.annotation.Configuration
import java.util.*
import java.util.stream.IntStream


val threadLocalList =  ThreadLocal<MutableList<Result>>()
val threadLocalStack =  ThreadLocal<Stack<Result>>()

data class Result(var name: String, var startTime: Long, var endTime: Long, var pauseTime: Long, var depth: Int)

@Configuration
@Aspect
class SpringStressTesterConfig {

    // TODO java option으로 패키지명을 받아서 aop의 포인트컷으로 쓰고 싶음..
    @Around("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Service) || " +
            "execution(public * org.springframework.data.repository.Repository+.*(..))" +
            "@within(org.springframework.stereotype.Repository)")
    fun doBasicProfiling(pjp: ProceedingJoinPoint): Any {
        val startTime = System.currentTimeMillis()
        val signature = pjp.signature as MethodSignature
        val signatureName = signature.declaringType.name
        push(startTime, signatureName)
        val retVal = pjp.proceed()
        val endTime = System.currentTimeMillis()
        pop(endTime, signatureName)

        if(threadLocalStack.get().empty()) {
            val list = threadLocalList.get()
            list.reverse()
            list.forEach{ v ->
                if(v.depth == 0) {
                    println("├──${v.name} : total: ${v.endTime - v.startTime}ms,  ${(v.endTime - v.startTime - v.pauseTime) }ms")
                } else {
                    val space = IntStream.range(0, v.depth * 4).mapToObj { " " }.reduce("", { a, b -> a + b })
                    println("│${space}└──${v.name} : total: ${v.endTime - v.startTime}ms, ${(v.endTime - v.startTime - v.pauseTime)}ms")
                }
            }
        }
        return retVal
    }

    fun push(startTime: Long, signatureName: String) {
        val stack = threadLocalStack.get() ?: let {
            val m = Stack<Result>()
            threadLocalStack.set(m)
            m
        }
        stack.push(Result(signatureName, startTime, 0L,0L, stack.size))
    }

    fun pop(endTime: Long, signatureName: String) {
        val stack = threadLocalStack.get()
        val result = stack.pop()
        result.endTime = endTime

        if(stack.size > 0) {
            val beforeResult = stack.pop()
            beforeResult.pauseTime += result.endTime - result.startTime
            stack.push(beforeResult)
        }

        val list = threadLocalList.get() ?: let {
            val list = mutableListOf<Result>()
            threadLocalList.set(list)
            list
        }
        list.add(result)
    }
}