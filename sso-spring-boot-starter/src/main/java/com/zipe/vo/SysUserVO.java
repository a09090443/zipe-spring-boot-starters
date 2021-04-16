package com.zipe.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author gary.tsai 2019/5/31
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String loginTime;

}
