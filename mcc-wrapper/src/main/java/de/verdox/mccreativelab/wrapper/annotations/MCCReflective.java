package de.verdox.mccreativelab.wrapper.annotations;

import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signals that this method / constructor may use java reflection to function properly based on its platform implementation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@SubtypeOf({GTENegativeOne.class})
public @interface MCCReflective {

}