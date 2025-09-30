import {Box, Link, Typography,} from '@mui/material';
import {Public,} from '@mui/icons-material';
import {KlabisTable, TableCell} from "../components/KlabisTable";
import MemberName from "../components/members/MemberName";
import {Actions} from "../components/Actions";
import EventType from "../components/events/EventType";

const EventsPage = () => {
    // Function to format date
    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return new Intl.DateTimeFormat('cs-CZ').format(date);
    };

    return (
        <Box>
            <Typography variant="h4" component="h1" gutterBottom>
                Akce
            </Typography>

            <KlabisTable api={"/events"} defaultOrderBy={"date"}>
                <TableCell sortable column={"date"} dataRender={({value}) => formatDate(value)}>Datum</TableCell>
                <TableCell sortable column={"name"}>Název</TableCell>
                <TableCell sortable column={"location"}>Místo</TableCell>
                <TableCell sortable column={"organizer"}>Pořadatel</TableCell>
                <TableCell column={"type"} dataRender={({value}) => <EventType eventType={value}/>}>Typ</TableCell>
                <TableCell column={"web"} dataRender={({value}) => <Link href={value}><Public/></Link>}>Web</TableCell>
                <TableCell sortable column={"registrationDeadline"} dataRender={({value}) => formatDate(value)}>Uzávěrka
                    přihlášek</TableCell>
                <TableCell column={"coordinator"} dataRender={({value}) => value ?
                    <MemberName memberId={value}/> : <>--</>}>Vedoucí</TableCell>
                <TableCell key="options" column="_actions" dataRender={props => (<></>)}>Možnosti</TableCell>
                <TableCell key="actions" column="_actions"
                           dataRender={props => (<Actions value={props.value}/>)}>Akce</TableCell>
            </KlabisTable>
        </Box>
    );
};

export default EventsPage;