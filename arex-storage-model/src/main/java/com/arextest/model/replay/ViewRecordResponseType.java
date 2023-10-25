package com.arextest.model.replay;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.response.DesensitizationResponseType;
import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Data
public class ViewRecordResponseType extends DesensitizationResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private List<AREXMocker> recordResult;
}