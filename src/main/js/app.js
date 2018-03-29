const React = require('react');
const ReactDOM = require('react-dom');
const client = require('./client');

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = { persons: [] };
    }

    componentDidMount() {
        client({ method: 'GET', path: '/data/persons' })
            .done(response => {
                this.setState( { persons: response.entity._embedded.persons } );
            });
    }

    render() {
        return (
            <PersonList persons={this.state.persons} />
        )
    }

}

class PersonList extends React.Component {
    render() {
        const persons = this.props.persons.map(person =>
            <Person key={person._links.self.href} person={person} />
        );

        return (
            <table>
                <tbody>
                    <tr>
                        <th>First Name</th>
                        <th>Last Name</th>
                    </tr>
                    {persons}
                </tbody>
            </table>
        )
    }
}

class Person extends React.Component {
    render() {
        return (
            <tr>
                <td>{this.props.person.firstName}</td>
                <td>{this.props.person.lastName}</td>
            </tr>
        )
    }
}

ReactDOM.render(
    <App />,
    document.getElementById('react')
);
