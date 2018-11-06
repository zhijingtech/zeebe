import React from 'react';
import {Input, LoadingIndicator} from 'components';
import {formatters} from 'services';
import classnames from 'classnames';

import './TypeaheadMultipleSelection.scss';

export default class TypeaheadMultipleSelection extends React.Component {
  constructor() {
    super();
    this.state = {
      searchQuery: ''
    };
    this.dragPlaceHolder = document.createElement('li');
    this.dragPlaceHolder.className = 'placeholder';
  }

  mapSelectedValues = values => {
    const isDraggable = !!this.props.onOrderChange;
    return (
      values.length > 0 && (
        <div className="TypeaheadMultipleSelection__labeled-valueList">
          <p>Selected {this.props.label}: </p>
          <div onDragOver={this.dragOver} className="TypeaheadMultipleSelection__values-sublist">
            {values.map((value, idx) => {
              return (
                <li
                  key={idx}
                  data-id={idx}
                  className={classnames('TypeaheadMultipleSelection__valueListItem', {
                    draggable: isDraggable
                  })}
                  draggable={isDraggable}
                  onDragEnd={this.dragEnd}
                  onDragStart={this.dragStart}
                >
                  <label>
                    <Input type="checkbox" checked value={idx} onChange={this.toggleSelected} />
                    {this.props.format(value)}
                  </label>
                </li>
              );
            })}
            <span className="endIndicator" data-id={values.length} />
          </div>
        </div>
      )
    );
  };

  dragStart = evt => {
    this.dragged = evt.currentTarget;
    this.cloneHeight = this.dragged.getBoundingClientRect().height;
    this.dragPlaceHolder.style.height = this.cloneHeight + 'px';
    evt.dataTransfer.effectAllowed = 'move';
    evt.dataTransfer.setData('Text', this.dragged.id);
  };

  dragEnd = evt => {
    this.dragged.style.display = 'block';
    this.dragged.parentNode.removeChild(this.dragPlaceHolder);

    // update props
    let data = this.props.selectedValues;
    const from = Number(this.dragged.dataset.id);
    let to = Number(this.over.dataset.id);
    if (from < to) to--;
    data.splice(to, 0, data.splice(from, 1)[0]);
    this.props.onOrderChange(data);
  };

  dragOver = evt => {
    evt.preventDefault();
    this.dragged.style.display = 'none';
    if (evt.target.className === 'placeholder') return;
    if (evt.target.nodeName === 'LI' || evt.target.className === 'endIndicator') {
      this.over = evt.target;
      evt.target.parentNode.insertBefore(this.dragPlaceHolder, evt.target);
    } else if (evt.target.nodeName === 'LABEL') {
      const listElement = evt.target.parentNode;
      this.over = listElement;
      listElement.parentNode.insertBefore(this.dragPlaceHolder, listElement);
    }
  };

  toggleSelected = ({target: {value, checked}}) =>
    this.props.toggleValue(this.props.selectedValues[value], checked);

  mapAvaliableValues = (availableValues, selectedValues) => {
    return (
      <div className="TypeaheadMultipleSelection__labeled-valueList">
        <p>Available {this.props.label}: </p>
        <div className="TypeaheadMultipleSelection__values-sublist">
          {availableValues.map((value, idx) => {
            if (!selectedValues.includes(value)) {
              return (
                <li key={idx} className="TypeaheadMultipleSelection__valueListItem">
                  <label>
                    <Input
                      type="checkbox"
                      checked={selectedValues.includes(value)}
                      value={idx}
                      onChange={this.toggleAvailable}
                    />
                    {formatters.getHighlightedText(
                      this.props.format(value),
                      this.state.searchQuery
                    )}
                  </label>
                </li>
              );
            }
            return null;
          })}
        </div>
      </div>
    );
  };

  toggleAvailable = ({target: {value, checked}}) =>
    this.props.toggleValue(this.props.availableValues[value], checked);

  render() {
    const {availableValues, selectedValues, loading} = this.props;
    const input = (
      <div className="TypeaheadMultipleSelection__labeled-input">
        <Input
          className="TypeaheadMultipleSelection__input"
          placeholder={`Search for ${this.props.label}`}
          onChange={e => {
            this.setState({searchQuery: e.target.value});
            return this.props.setFilter(e);
          }}
        />
      </div>
    );
    const loadingIndicator = loading ? <LoadingIndicator /> : '';
    if (availableValues.length === 0) {
      return (
        <div className="TypeaheadMultipleSelection">
          {input}
          {loadingIndicator}
          <div className="TypeaheadMultipleSelection__valueList">
            {this.mapSelectedValues(selectedValues)}
          </div>
          <div className="TypeaheadMultipleSelection__valueList">
            <div className="TypeaheadMultipleSelection__labeled-valueList">
              <p>Available {this.props.label}: </p>
              <li className="TypeaheadMultipleSelection__no-items">
                {loading ? '' : `No matching ${this.props.label} found`}
              </li>
            </div>
          </div>
        </div>
      );
    }
    return (
      <div className="TypeaheadMultipleSelection">
        {input}
        {loadingIndicator}
        <div className="TypeaheadMultipleSelection__valueList">
          {this.mapSelectedValues(selectedValues)}
          {this.mapAvaliableValues(availableValues, selectedValues)}
        </div>
      </div>
    );
  }
}

TypeaheadMultipleSelection.defaultProps = {
  format: v => v
};
