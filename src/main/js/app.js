import '../scss/custom.scss'

import React from 'react'
import ReactDOM from 'react-dom'
import {
    Container
} from 'reactstrap';

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = { occupations: [], attributes: [], pageSize: 20, links: {} };
    }

    componentDidMount() {

    }

    render() {
        return (
            <Container>
                Hello
            </Container>
        )
    }
}

ReactDOM.render(
    <App />,
    document.getElementById('root')
);
