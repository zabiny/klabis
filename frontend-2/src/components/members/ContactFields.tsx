import React from "react";
import {Grid, TextField} from "@mui/material";

type Contact = {
    email: string;
    phone: string;
    note: string;
};

type ContactFieldsProps = {
    value: Contact;
    onChange: (val: Contact) => void;
    showNote?: boolean;
};

export const ContactFields: React.FC<ContactFieldsProps> = ({
                                                                value,
                                                                onChange,
                                                                showNote = true,
                                                            }) => {
    const handleFieldChange = (field: keyof Contact) => (
        e: React.ChangeEvent<HTMLInputElement>
    ) => {
        onChange({...value, [field]: e.target.value});
    };

    return (
        <>
            <Grid item xs={12} md={5}>
                <TextField
                    label="E-mail"
                    value={value.email}
                    onChange={handleFieldChange("email")}
                    fullWidth
                    type="email"
                />
            </Grid>
            <Grid item xs={12} md={4}>
                <TextField
                    label="Telefon"
                    value={value.phone}
                    onChange={handleFieldChange("phone")}
                    fullWidth
                />
            </Grid>
            {showNote && (
                <Grid item xs={12} md={3}>
                    <TextField
                        label="Poznámka"
                        value={value.note}
                        onChange={handleFieldChange("note")}
                        fullWidth
                    />
                </Grid>
            )}
        </>
    );
};

export default ContactFields;
