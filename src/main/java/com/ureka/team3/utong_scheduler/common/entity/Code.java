package com.ureka.team3.utong_scheduler.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "code")
@IdClass(CodeId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Code {

    @Id
    @Column(name = "group_code", length = 3, nullable = false)
    private String groupCode;

    @Id
    @Column(name = "code", length = 3, nullable = false)
    private String code;

    @Column(name = "code_name", length = 50, nullable = false)
    private String codeName;

    @Column(name = "code_name_brief", length = 200)
    private String codeNameBrief;

    @Column(name = "order_no")
    private Integer orderNo;
}
