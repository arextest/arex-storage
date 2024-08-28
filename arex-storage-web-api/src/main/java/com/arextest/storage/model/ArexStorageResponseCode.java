package com.arextest.storage.model;

import com.arextest.common.model.response.ResponseCode_New;

public class ArexStorageResponseCode extends ResponseCode_New {




  // app auth error codes start with 105xxx, shared with arex-saas-api
  // com.arextest.web.common.exception.ArexApiResponseCode
  public static final int APP_AUTH_NO_APP_ID = 105001;
  public static final int APP_AUTH_ERROR_APP_ID = 105002;
  public static final int APP_AUTH_NO_PERMISSION = 105003;

}
