import React from 'react';
import {mount} from 'enzyme';

import InstancesContainer from './InstancesContainer';
import Instances from './Instances';

import {formatDiagramNodes, parseQueryString} from './service';
import * as api from 'modules/api/instances/instances';
import * as apiDiagram from 'modules/api/diagram/diagram';
import {mockResolvedAsyncFn, flushPromises} from 'modules/testUtils';
import {getFilterQueryString} from 'modules/utils/filter';
import {DEFAULT_FILTER} from 'modules/constants';
import {getDiagramNodes, groupedWorkflowsMock} from 'modules/testUtils';
const InstancesContainerWrapped = InstancesContainer.WrappedComponent;

// component mocks
jest.mock(
  './Instances',
  () =>
    function Instances(props) {
      return <div />;
    }
);

// props mocks
const fullFilterWithoutWorkflow = {
  active: true,
  incidents: true,
  completed: true,
  finished: true,
  ids: '424242, 434343',
  errorMessage: 'loremIpsum',
  startDate: '28 December 2018',
  endDate: '28 December 2018'
};

const fullFilterWithWorkflow = {
  active: true,
  incidents: true,
  completed: true,
  finished: true,
  ids: '424242, 434343',
  errorMessage: 'loremIpsum',
  startDate: '28 December 2018',
  endDate: '28 December 2018',
  workflow: 'demoProcess',
  version: 1,
  activityId: 'taskD'
};

const localStorageProps = {
  getStateLocally: jest.fn(),
  storeStateLocally: jest.fn()
};

// api mocks
api.fetchGroupedWorkflowInstances = mockResolvedAsyncFn(groupedWorkflowsMock);
apiDiagram.fetchWorkflowXML = mockResolvedAsyncFn('<xml />');
jest.mock('bpmn-js', () => ({}));
jest.mock('modules/utils/bpmn');

// local utility
const pushMock = jest.fn();
function getRouterProps(filter = DEFAULT_FILTER) {
  return {
    history: {push: pushMock},
    location: {
      search: getFilterQueryString(filter)
    }
  };
}

describe('InstancesContainer', () => {
  afterEach(() => {
    pushMock.mockClear();
  });

  it('should fetch the groupedWorkflowInstances', async () => {
    // given
    const node = mount(
      <InstancesContainerWrapped {...localStorageProps} {...getRouterProps()} />
    );

    //when
    await flushPromises();
    node.update();

    // then
    expect(api.fetchGroupedWorkflowInstances).toHaveBeenCalled();
  });

  it('should write the filter to local storage', async () => {
    // given
    const node = mount(<InstancesContainer {...getRouterProps()} />);

    //when
    await flushPromises();
    node.update();

    // then
    expect(localStorage.setItem).toHaveBeenCalled();
  });

  it('should render the Instances', () => {
    const node = mount(
      <InstancesContainerWrapped {...localStorageProps} {...getRouterProps()} />
    );

    expect(node.find(Instances)).toExist();
  });

  it('should pass data to Instances for default filter', async () => {
    // given
    const node = mount(
      <InstancesContainerWrapped {...localStorageProps} {...getRouterProps()} />
    );

    //when
    await flushPromises();
    node.update();

    const InstancesNode = node.find(Instances);

    // then
    expect(
      InstancesNode.prop('groupedWorkflowInstances')[
        groupedWorkflowsMock[0].bpmnProcessId
      ]
    ).not.toBe(undefined);
    expect(
      InstancesNode.prop('groupedWorkflowInstances')[
        groupedWorkflowsMock[1].bpmnProcessId
      ]
    ).not.toBe(undefined);
    expect(InstancesNode.prop('filter')).toEqual(DEFAULT_FILTER);
    expect(InstancesNode.prop('diagramWorkflow')).toBe(
      node.state().currentWorkflow
    );
    expect(InstancesNode.props().onFilterChange).toBe(
      node.instance().setFilterInURL
    );
    expect(InstancesNode.props().diagramNodes).toEqual([]);
  });

  it('should pass data to Instances for full filter, without workflow data', async () => {
    // given
    const node = mount(
      <InstancesContainerWrapped
        {...localStorageProps}
        {...getRouterProps(fullFilterWithoutWorkflow)}
      />
    );

    //when
    await flushPromises();
    node.update();

    const InstancesNode = node.find(Instances);

    expect(InstancesNode.prop('filter')).toEqual(fullFilterWithoutWorkflow);
    expect(InstancesNode.prop('diagramWorkflow')).toEqual({});
    expect(InstancesNode.prop('diagramNodes')).toEqual([]);
  });
  it('should pass data to Instances for full filter, with workflow data', async () => {
    const node = mount(
      <InstancesContainerWrapped
        {...localStorageProps}
        {...getRouterProps(fullFilterWithWorkflow)}
      />
    );

    //when
    await flushPromises();
    node.update();

    const InstancesNode = node.find(Instances);

    expect(InstancesNode.prop('filter')).toEqual(fullFilterWithWorkflow);
    expect(InstancesNode.prop('diagramWorkflow')).toEqual(
      groupedWorkflowsMock[0].workflows[2]
    );
    expect(InstancesNode.prop('diagramNodes')).toEqual(
      formatDiagramNodes(getDiagramNodes())
    );
  });
  it('should pass data to Instances for full filter, with all versions', async () => {
    const {activityId, version, ...rest} = fullFilterWithWorkflow;
    const node = mount(
      <InstancesContainerWrapped
        {...localStorageProps}
        {...getRouterProps({
          ...rest,
          version: 'all'
        })}
      />
    );

    //when
    await flushPromises();
    node.update();

    const InstancesNode = node.find(Instances);
    expect(InstancesNode.prop('filter')).toEqual({
      ...rest,
      version: 'all'
    });
    expect(InstancesNode.prop('diagramWorkflow')).toEqual({});
    expect(InstancesNode.prop('diagramNodes')).toEqual([]);
  });

  describe('should fix an invalid filter in url', () => {
    it('should add the default filter to the url when no filter is present', async () => {
      const noFilterRouterProps = {
        history: {push: jest.fn()},
        location: {
          search: ''
        }
      };

      mount(
        <InstancesContainerWrapped
          {...localStorageProps}
          {...noFilterRouterProps}
        />
      );

      //when
      await flushPromises();

      expect(noFilterRouterProps.history.push).toHaveBeenCalled();
      expect(noFilterRouterProps.history.push.mock.calls[0][0].search).toBe(
        '?filter={"active":true,"incidents":true}'
      );
    });

    it('when the filter in url is invalid', async () => {
      const invalidFilterRouterProps = {
        history: {push: jest.fn()},
        location: {
          search:
            '?filter={"active": fallse, "errorMessage": "No more retries left"'
        }
      };
      const node = mount(
        <InstancesContainerWrapped
          {...localStorageProps}
          {...invalidFilterRouterProps}
        />
      );

      //when
      await flushPromises();
      node.update();

      expect(invalidFilterRouterProps.history.push).toHaveBeenCalled();
      expect(
        invalidFilterRouterProps.history.push.mock.calls[0][0].search
      ).toBe('?filter={"active":true,"incidents":true}');
    });

    it('when the workflow in url is invalid', async () => {
      const node = mount(
        <InstancesContainerWrapped
          {...localStorageProps}
          {...getRouterProps({
            ...fullFilterWithWorkflow,
            workflow: 'x'
          })}
        />
      );

      //when
      await flushPromises();
      node.update();

      // expect invalid activityId to have been removed
      expect(pushMock).toHaveBeenCalled();
      const search = pushMock.mock.calls[0][0].search;
      const {version, workflow, activityId, ...rest} = fullFilterWithWorkflow;

      expect(parseQueryString(search).filter).toEqual(rest);
    });

    it('when the version in url is invalid', async () => {
      const node = mount(
        <InstancesContainerWrapped
          {...localStorageProps}
          {...getRouterProps({
            ...fullFilterWithWorkflow,
            version: 'x'
          })}
        />
      );

      //when
      await flushPromises();
      node.update();

      // expect invalid activityId to have been removed
      expect(pushMock).toHaveBeenCalled();
      const search = pushMock.mock.calls[0][0].search;
      const {version, workflow, activityId, ...rest} = fullFilterWithWorkflow;

      expect(parseQueryString(search).filter).toEqual(rest);
    });

    it('when the activityId in url is invalid', async () => {
      const node = mount(
        <InstancesContainerWrapped
          {...localStorageProps}
          {...getRouterProps({
            ...fullFilterWithWorkflow,
            activityId: 'x'
          })}
        />
      );

      //when
      await flushPromises();
      node.update();

      // expect invalid activityId to have been removed
      expect(pushMock).toHaveBeenCalledTimes(1);
      const search = pushMock.mock.calls[0][0].search;
      const {activityId, ...rest} = fullFilterWithWorkflow;
      expect(parseQueryString(search).filter).toEqual(rest);
    });

    it('should remove activityId when version="all"', async () => {
      const node = mount(
        <InstancesContainerWrapped
          {...localStorageProps}
          {...getRouterProps({
            ...fullFilterWithWorkflow,
            version: 'all'
          })}
        />
      );

      //when
      await flushPromises();
      node.update();

      // expect invalid activityId to have been removed
      expect(pushMock).toHaveBeenCalledTimes(1);
      const search = pushMock.mock.calls[0][0].search;
      const {activityId, version, ...rest} = fullFilterWithWorkflow;
      expect(parseQueryString(search).filter).toEqual({
        ...rest,
        version: 'all'
      });
    });
  });
});
