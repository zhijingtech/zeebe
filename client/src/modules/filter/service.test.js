/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {filterSameTypeExistingFilters} from './service';

it('should remove the filters of the same type and the same level', () => {
  const existingFilters = [
    {filterLevel: 'instance', type: 'startDate'},
    {filterLevel: 'view', type: 'includesOpenIncident'},
  ];

  expect(
    filterSameTypeExistingFilters(existingFilters, {
      filterLevel: 'instance',
      type: 'startDate',
    })
  ).toEqual([{filterLevel: 'view', type: 'includesOpenIncident'}]);

  expect(
    filterSameTypeExistingFilters(existingFilters, {
      filterLevel: 'instance',
      type: 'includesOpenIncident',
    })
  ).toEqual(existingFilters);
});

it('should keep filters of the same type and level if they apply to different definitions', () => {
  const existingFilters = [
    {filterLevel: 'instance', type: 'startDate', appliedTo: ['definition1']},
    {filterLevel: 'view', type: 'includesOpenIncident', appliedTo: ['definition1']},
  ];

  expect(
    filterSameTypeExistingFilters(existingFilters, {
      filterLevel: 'instance',
      type: 'startDate',
      appliedTo: ['definition2'],
    })
  ).toEqual(existingFilters);
});
