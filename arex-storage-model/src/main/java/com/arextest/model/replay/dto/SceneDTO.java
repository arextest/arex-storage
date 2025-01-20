package com.arextest.model.replay.dto;

import java.util.List;
import lombok.Data;

/**
 * SceneDTO
 *
 * @author xinyuan_wang
 * @date 2024/7/29 16:44
 */
@Data
public class SceneDTO {
    private List<String> sceneList;
    private Long total;
    private String lastId;
}
