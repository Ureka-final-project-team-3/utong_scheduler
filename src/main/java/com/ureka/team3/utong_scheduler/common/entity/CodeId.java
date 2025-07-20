package com.ureka.team3.utong_scheduler.common.entity;

import java.io.Serializable;
import java.util.Objects;

public class CodeId implements Serializable {

    private String groupCode;
    private String code;

    public CodeId() {}

    public CodeId(String groupCode, String code) {
        this.groupCode = groupCode;
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CodeId)) return false;
        CodeId that = (CodeId) o;
        return Objects.equals(groupCode, that.groupCode) &&
               Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupCode, code);
    }
}
