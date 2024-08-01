package com.arextest.model.replay.dto;

import com.arextest.model.scenepool.Scene;
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
    private List<Scene> sceneList;
    private Long total;
}
