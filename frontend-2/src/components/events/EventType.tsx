import {type ReactNode} from "react";

type EventT = 'TRAINING' | 'COMPETITION';

interface EventTypeProps {
    eventType: EventT;
}

const translate = (name: EventT): ReactNode => {
    if (!name) {
        return "";
    }
    if (name == "TRAINING") {
        return "Trénink";
    }

    if (name == "COMPETITION") {
        return "Závod";
    }

    return "Neznámý typ";
}

function EventType({eventType}: EventTypeProps) {

    return (
        <span>{translate(eventType)}</span>
    );
}

export default EventType;