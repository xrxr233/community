package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被该注解修饰的方法需要登录才能访问
 * 通过检查访问的方法是否被该注解修饰，从而判断方法是否需要登录才能访问
 *
 * 注意：该注解实际上是拦截功能的一种实现方式，通过在WebMvcConfig中配置includePathPatterns同样可以实现
 * 相比于includePathPatterns的方式，该方法更方便，因为如果方法很多，在includePathPatterns时添加的是方法名字符串，容易遗漏出错
 * */
@Target(ElementType.METHOD)  //修饰方法
@Retention(RetentionPolicy.RUNTIME)  //运行时有效
public @interface LoginRequired {

}
