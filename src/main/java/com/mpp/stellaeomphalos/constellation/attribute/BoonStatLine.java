package com.mpp.stellaeomphalos.constellation.attribute;

/** 不可变展示值：属性、名称键快照、值、后缀、后处理说明。 */
public record BoonStatLine(BoonAttribute attribute, String nameKey, double value, String suffix, String note) {

    public BoonStatLine {
        if (attribute == null || nameKey == null) throw new IllegalArgumentException("null attribute/nameKey");
        suffix = suffix == null ? "" : suffix;
        note = note == null ? "" : note;
    }
}
