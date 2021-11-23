/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React, {useEffect} from 'react';
import {observer} from 'mobx-react';

import {
  Panel,
  Title,
  InstancesBar,
  SkeletonBar,
  LabelContainer,
  Label,
} from './styled';
import {statisticsStore} from 'modules/stores/statistics';
import {StatusMessage} from 'modules/components/StatusMessage';
import {Locations} from 'modules/routes';
import {panelStatesStore} from 'modules/stores/panelStates';
import {tracking} from 'modules/tracking';

const MetricPanel = observer(() => {
  const {running, active, withIncidents, status} = statisticsStore.state;

  useEffect(() => {
    statisticsStore.init();

    return () => {
      statisticsStore.reset();
    };
  }, []);

  if (status === 'error') {
    return (
      <StatusMessage variant="error">
        Process statistics could not be fetched
      </StatusMessage>
    );
  }

  return (
    <Panel data-testid="metric-panel">
      <Title
        data-testid="total-instances-link"
        onClick={() => {
          panelStatesStore.expandFiltersPanel();
          tracking.track({
            eventName: 'navigation',
            link: 'dashboard-running-instances',
          });
        }}
        to={(location) =>
          Locations.filters(
            location,
            running === 0
              ? {
                  completed: true,
                  canceled: true,
                  incidents: true,
                  active: true,
                }
              : {
                  incidents: true,
                  active: true,
                }
          )
        }
      >
        {`${
          status === 'fetched' ? `${running} ` : ''
        }Running Instances in total`}
      </Title>
      {status === 'fetched' && (
        <InstancesBar
          incidentsCount={withIncidents}
          activeCount={active}
          size="large"
          barHeight={15}
        />
      )}
      {(status === 'initial' || status === 'first-fetch') && (
        <SkeletonBar data-testid="instances-bar-skeleton" />
      )}

      <LabelContainer>
        <Label
          data-testid="incident-instances-link"
          onClick={() => {
            panelStatesStore.expandFiltersPanel();
            tracking.track({
              eventName: 'navigation',
              link: 'dashboard-instances-with-incidents',
            });
          }}
          to={(location) =>
            Locations.filters(location, {
              incidents: true,
            })
          }
        >
          Instances with Incident
        </Label>
        <Label
          data-testid="active-instances-link"
          onClick={() => {
            panelStatesStore.expandFiltersPanel();
            tracking.track({
              eventName: 'navigation',
              link: 'dashboard-active-instances',
            });
          }}
          to={(location) =>
            Locations.filters(location, {
              active: true,
            })
          }
        >
          Active Instances
        </Label>
      </LabelContainer>
    </Panel>
  );
});

export {MetricPanel};
