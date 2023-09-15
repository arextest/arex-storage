package com.arextest.config.model.dto.application;

/**
 * All value of fields not null
 *
 * @author jmo
 * @since 2021/12/21
 */
public interface ApplicationDescription {

    String getAppId();

    String getOwner();

    String getAppName();

    String getOrganizationName();

    String getOrganizationId();

    String getDescription();

    String getGroupName();

    String getGroupId();

    String getCategory();
}
