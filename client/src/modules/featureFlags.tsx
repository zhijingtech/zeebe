/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

const IS_PROCESS_INSTANCES_ENABLED = process.env.NODE_ENV === 'development';
const IS_MULTI_TENANCY_ENABLED = process.env.NODE_ENV === 'development';

export {IS_PROCESS_INSTANCES_ENABLED, IS_MULTI_TENANCY_ENABLED};
