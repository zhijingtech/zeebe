/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import {NavItem} from 'components';

import './HeaderNav.scss';

export default function HeaderNav(props) {
  return (
    <ul role="navigation" className="HeaderNav">
      {props.children}
    </ul>
  );
}

HeaderNav.Item = NavItem;
