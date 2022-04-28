package com.arextest.storage.model.replay;

import com.arextest.storage.model.Response;
import com.arextest.storage.model.header.ResponseStatusType;
import com.arextest.storage.model.mocker.MainEntry;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public class PagingQueryCaseResponseType implements Response {
    private ResponseStatusType responseStatusType;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private List<? extends MainEntry> mainEntryList;
}
