package com.arextest.model.replay;

import com.arextest.model.response.ResponseStatusType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.model.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Setter
@Getter
public class QueryReplayResultResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private List<ListResultHolder> resultHolderList;
}