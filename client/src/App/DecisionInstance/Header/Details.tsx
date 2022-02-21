/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {Link} from 'modules/components/Link';
import {Locations} from 'modules/routes';
import {DecisionInstanceType} from 'modules/stores/decisionInstance';
import {formatDate} from 'modules/utils/date/formatDate';
import {useParams} from 'react-router-dom';
import {Table, TD, TH, SkeletonBlock} from './styled';

type Props = {
  decisionInstance?: DecisionInstanceType;
  'data-testid'?: string;
};

const Details: React.FC<Props> = ({decisionInstance, ...props}) => {
  const {decisionInstanceId} = useParams<{decisionInstanceId: string}>();

  return (
    <Table data-testid={props['data-testid']}>
      <thead>
        <tr>
          <TH>Decision</TH>
          <TH>Decision Instance ID</TH>
          <TH>Version</TH>
          <TH>Evaluation Time</TH>
          <TH>Process Instance ID</TH>
        </tr>
      </thead>
      <tbody>
        {decisionInstance === undefined ? (
          <tr>
            <TD>
              <SkeletonBlock $width="200px" />
            </TD>
            <TD>
              <SkeletonBlock $width="162px" />
            </TD>
            <TD>
              <SkeletonBlock $width="17px" />
            </TD>
            <TD>
              <SkeletonBlock $width="151px" />
            </TD>
            <TD>
              <SkeletonBlock $width="162px" />
            </TD>
          </tr>
        ) : (
          <tr>
            <TD>{decisionInstance.name}</TD>
            <TD>{decisionInstanceId}</TD>
            <TD>{decisionInstance.version}</TD>
            <TD>{formatDate(decisionInstance.evaluationDate)}</TD>
            <TD>
              {decisionInstance.processInstanceId ? (
                <Link
                  to={(location) =>
                    Locations.instance(
                      decisionInstance.processInstanceId as string,
                      location
                    )
                  }
                  title={`View process instance ${decisionInstance.processInstanceId}`}
                >
                  {decisionInstance.processInstanceId}
                </Link>
              ) : (
                'None'
              )}
            </TD>
          </tr>
        )}
      </tbody>
    </Table>
  );
};

export {Details};
