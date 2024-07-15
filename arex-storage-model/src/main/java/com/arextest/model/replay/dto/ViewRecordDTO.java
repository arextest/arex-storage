package com.arextest.model.replay.dto;

import com.arextest.model.mock.AREXMocker;
import java.util.List;
import lombok.Data;

/**
 * ViewRecordDto
 *
 * @author xinyuan_wang
 * @date 2024/7/15 16:44
 */
@Data
public class ViewRecordDTO {
    private List<AREXMocker> recordResult;
    private List<AREXMocker> replayResult;
    private String sourceProvider;
}
