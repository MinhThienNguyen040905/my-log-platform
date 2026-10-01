package com.mylog.selfcare.application.query;

import java.math.BigDecimal;

public record MoodAssociationView(int completedDays, int otherDays, BigDecimal averageMoodDifference,
                                  String interpretation) {}
