import React from 'react'
import ReactDOM from 'react-dom'
import client from './client'
import follow from './follow'

const root = '/data';

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = { occupations: [], attributes: [], pageSize: 20, links: {} };
    }

    componentDidMount() {
        this.loadFromServer(this.state.pageSize);
    }

    loadFromServer(pageSize) {
        follow(client, root, [{rel: 'occupations', params: {size: pageSize}}])
            .then(occupationCollection => {
                return client({
                    method: 'GET',
                    path: occupationCollection.entity._links.profile.href,
                    headers: {'Accept': 'application/schema+json'}
                }).then(schema => {
                    this.schema = schema.entity;
                    return occupationCollection;
                })
            }).then(occupationCollection => {
                this.setState({
                    occupations: occupationCollection.entity._embedded.occupations,
                    attributes: Object.keys(this.schema.properties),
                    pageSize: pageSize,
                    links: occupationCollection.entity._links});
                });
    }

    onCreate(newOccupation) {
        follow(client, root, ['occupations']).then(occupationCollection => {
            return client({
                method: 'POST',
                path: occupationCollection.entity._links.self.href,
                entity: newOccupation,
                headers: { 'Content-Type': 'application/json' }
            })
        }).then(response => {
            return follow(client, root, [
                { rel: 'occupations', params: { 'size' : this.state.pageSize } }
            ]);
        }).then(response => {
            if (typeof response.entity._links.last !== 'undefined') {
                this.onNavigate(response.entity._links.last.href);
            } else {
                this.onNavigate(response.entity._links.self.href);
            }
        });
    }

    render() {
        return (
            <div>
                <CreateOccupationDialog attributes={this.state.attributes} onCreate={this.onCreate}/>
                <OccupationList occupations={this.state.occupations} />
            </div>
        )
    }
}

class OccupationList extends React.Component {
    render() {
        const occupations = this.props.occupations.map(occupation =>
            <Occupation key={occupation._links.self.href} occupation={occupation} />
        );

        return (
            <table>
                <tbody>
                    <tr>
                        <th>Name</th>
                        <th>Support Factor</th>
                        <th>Male</th>
                        <th>Female</th>
                        <th>Rural</th>
                    </tr>
                    {occupations}
                </tbody>
            </table>
        )
    }
}

class Occupation extends React.Component {
    render() {
        return (
            <tr>
                <td>{this.props.occupation.name}</td>
                <td>{this.props.occupation.supportFactor}</td>
                <td>{this.props.occupation.allowMale ? 'X' : '' }</td>
                <td>{this.props.occupation.allowFemale ? 'X' : ''}</td>
                <td>{this.props.occupation.rural ? 'X' : ''}</td>
            </tr>
        )
    }
}

class CreateOccupationDialog extends React.Component {
    constructor(props) {
        super(props);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleSubmit(e) {
        e.preventDefault();
        let newOccupation = {};
        this.props.attributes.forEach(attribute => {
            newOccupation[attribute] = ReactDOM.findDOMNode(this.refs[attribute]).value.trim();
        });
        this.props.onCreate(newOccupation);

        // clear out dialog's inputs
        this.props.attributes.forEach(attribute => {
            ReactDOM.findDOMNode(this.refs[attribute]).value = '';
        });

        // Navigate away from the dialog to hide it
        window.location = '#';
    }

    render() {
        const inputs = this.props.attributes.map(attribute =>
            <p key={attribute}>
                <input type="text" placeholder={attribute} ref={attribute} className={"field"} />
            </p>
        );

        return (
            <div>
                <a href="#createOccupation">Create</a>

                <div id="createOccupation" className="modalDialog">
                    <div>
                        <a href="#" title="Close" className="close">X</a>

                        <h2>Create new occupation</h2>

                        <form>
                            {inputs}
                            <button onClick={this.handleSubmit}>Create</button>
                        </form>
                    </div>
                </div>
            </div>
        )
    }
}

ReactDOM.render(
    <App />,
    document.getElementById('react')
);
