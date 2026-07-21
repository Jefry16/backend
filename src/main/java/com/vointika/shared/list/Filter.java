package com.vointika.shared.list;

public record Filter(String field, FilterOp op, Object value) {}
