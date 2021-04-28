package com.zipe.quartz.enums;

public enum ScheduleJobStatusEnum {

    /**
     * 若Job不存在新增一個Job和Trigger, 否則更新Trigger
     */
    MERGE(4),
	DELETE(0),
	START(1),
	SUSPEND(2),
	RESUME(3),
	RUNNING(5),
	ERROR(6),
	COMPLETE(7),
	ONCE(8);

	private int status;

    ScheduleJobStatusEnum(int status) {
        this.status = status;
    }

	public int status() {
		return status;
	}

}
