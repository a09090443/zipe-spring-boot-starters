package com.zipe.quartz.enums;

/**
 * 排程任務狀態列舉。
 *
 * <p>定義 Quartz 排程任務的各種操作指令與執行狀態，
 * 供 {@code QuartzJobUtil} 及相關控制器判斷要對排程執行何種動作。</p>
 */
public enum ScheduleJobStatusEnum {

    /**
     * 若Job不存在新增一個Job和Trigger, 否則更新Trigger
     */
    MERGE(4),

    /**
     * 刪除排程任務及其對應的 Trigger。
     */
	DELETE(0),

    /**
     * 啟動排程任務。
     */
	START(1),

    /**
     * 暫停排程任務，保留 Trigger 但停止觸發。
     */
	PAUSE(2),

    /**
     * 恢復已暫停的排程任務，使其繼續依 Trigger 排程執行。
     */
	RESUME(3),

    /**
     * 排程任務執行中。
     */
	RUNNING(5),

    /**
     * 排程任務執行發生錯誤。
     */
	ERROR(6),

    /**
     * 排程任務已執行完畢。
     */
	COMPLETE(7),

    /**
     * 僅執行一次的排程任務。
     */
	ONCE(8);

    /** 對應至資料庫或業務邏輯層使用的整數狀態碼。 */
	private int status;

    /**
     * 以指定的整數狀態碼建立列舉常數。
     *
     * @param status 對應的整數狀態碼
     */
    ScheduleJobStatusEnum(int status) {
        this.status = status;
    }

    /**
     * 取得此列舉常數對應的整數狀態碼。
     *
     * @return 整數狀態碼
     */
	public int status() {
		return status;
	}

}
