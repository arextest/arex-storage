package com.arextest.model.replay;

import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class QueryReplayResultResponseType implements Response {

  private ResponseStatusType responseStatusType;
  private List<ListResultHolder> resultHolderList;
  private List<CompareReplayResult> replayResults;
  private Boolean invalidResult;
  private Boolean needMatch;
}