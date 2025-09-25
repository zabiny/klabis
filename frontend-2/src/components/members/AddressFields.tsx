import React from "react";
import {Grid, MenuItem, TextField} from "@mui/material";

type Address = {
    streetAndNumber: string;
    city: string;
    postalCode: string;
    country: string;
};

type AddressFieldsProps = {
    value: Address;
    onChange: (val: Address) => void;
    countryOptions?: { value: string; label: string }[];
};

const DEFAULT_COUNTRIES = [
    {value: "", label: ""},
    {value: "CZ", label: "Česká republika"},
    {value: "SK", label: "Slovensko"},
    {value: "DE", label: "Německo"},
    {value: "AT", label: "Rakousko"},
    {value: "PL", label: "Polsko"},
    {value: "AS", label: "Americká Samoa"},
    {value: "IO", label: "Britské indickooceánské území"},
];

export const AddressFields: React.FC<AddressFieldsProps> = ({
                                                                value,
                                                                onChange,
                                                                countryOptions = DEFAULT_COUNTRIES,
                                                            }) => {
    const handleFieldChange = (field: keyof Address) => (
        e: React.ChangeEvent<HTMLInputElement>
    ) => {
        onChange({...value, [field]: e.target.value});
    };

    return (
        <>
            <Grid item xs={12} md={4}>
                <TextField
                    label="Ulice a číslo"
                    value={value.streetAndNumber}
                    onChange={handleFieldChange("streetAndNumber")}
                    fullWidth
                    required
                />
            </Grid>
            <Grid item xs={12} md={3}>
                <TextField
                    label="Město"
                    value={value.city}
                    onChange={handleFieldChange("city")}
                    fullWidth
                    required
                />
            </Grid>
            <Grid item xs={12} md={2}>
                <TextField
                    label="PSČ"
                    value={value.postalCode}
                    onChange={handleFieldChange("postalCode")}
                    fullWidth
                />
            </Grid>
            <Grid item xs={12} md={3}>
                <TextField
                    label="Stát"
                    select
                    value={value.country}
                    onChange={handleFieldChange("country")}
                    fullWidth
                    required
                >
                    {countryOptions.map(o => (
                        <MenuItem key={o.value} value={o.value}>
                            {o.label}
                        </MenuItem>
                    ))}
                </TextField>
            </Grid>
        </>
    );
};

export default AddressFields;
