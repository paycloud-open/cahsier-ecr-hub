package com.smartpos.demo.sample;

import com.smartpos.demo.ConnectOptions;
import com.smartpos.demo.WiseException;

/**
 * @author hongge
 * @Date ${date} ${time}
 * @Note
 */
public class ECRRetryPolicy implements ConnectOptions.IRetryPolicy {


    @Override
    public long waitingTime(WiseException e) {

        int reasonCode = e.getReasonCode();

        if (WiseException.REASON_CODE_BLUETOOTH_NOT_SUPPORT_ERROR == reasonCode ||
                WiseException.REASON_CODE_MAC_ERROR == reasonCode) {
            return ConnectOptions.RETRY_STOP;
        } else {
            return 1000;
        }

    }
}
