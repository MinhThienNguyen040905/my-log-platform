package com.mylog.analysis.application;

/** A provider configuration or contract failure that retrying the same job cannot repair. */
public class PermanentAnalysisFailureException extends RuntimeException {}
