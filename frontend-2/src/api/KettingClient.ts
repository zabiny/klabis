import {Ketting} from "ketting";
import React, {useContext} from "react";

const client = new Ketting("/api");

export const KettingContext = React.createContext<Ketting>(client);

export const useKettingClient = () => {
    return useContext(KettingContext);
}