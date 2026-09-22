package com.mylog.analysis.entity;

public record SafetyDecision(String riskLevel, boolean blocksNormalResponse, String actionTaken) {}
