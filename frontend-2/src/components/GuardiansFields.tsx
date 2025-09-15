import React from "react";
import {Button, Grid, Paper, Stack, TextField, Typography} from "@mui/material";
import ContactFields from "./ContactFields";
import type {MemberRegistrationForm} from "../api/membersApi";

type Guardian = NonNullable<MemberRegistrationForm['guardians']>[number];

export interface GuardiansFieldsProps {
    value: Guardian[];
    onChange: (guardians: Guardian[]) => void;
    disabled?: boolean;
}

// Komponenta pro jednoho zástupce
interface GuardianFieldProps {
    value: Guardian;
    onChange: (g: Guardian) => void;
    onRemove: () => void;
    disabled?: boolean;
}

const GuardianField: React.FC<GuardianFieldProps> = ({value, onChange, onRemove, disabled}) => {
    // Přímé změny hodnot
    const handleChange = (path: string, v: any) => {
        const keys = path.split('.');
        let obj: any = {...value};
        let ref = obj;
        for (let k = 0; k < keys.length - 1; k++) {
            if (!(keys[k] in ref)) ref[keys[k]] = {};
            ref = ref[keys[k]];
        }
        ref[keys[keys.length - 1]] = v;
        onChange(obj);
    };

    return (
        <Paper
            variant="outlined"
            sx={{p: 2, bgcolor: "#f9f9f9", position: "relative"}}
        >
            <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                    <TextField
                        fullWidth
                        label="Jméno"
                        value={value.firstName ?? ''}
                        onChange={e => handleChange('firstName', e.target.value)}
                        required
                    />
                </Grid>
                <Grid item xs={12} sm={6}>
                    <TextField
                        fullWidth
                        label="Příjmení"
                        value={value.lastName ?? ''}
                        onChange={e => handleChange('lastName', e.target.value)}
                        required
                    />
                </Grid>
                <ContactFields
                    value={{
                        email: value.contact?.email || '',
                        phone: value.contact?.phone || '',
                        note: value.contact?.note || '',
                    }}
                    onChange={c => handleChange('contact', c)}
                />
                <Grid item xs={12}>
                    <TextField
                        fullWidth
                        label="Poznámka k zástupci"
                        value={value.note || ''}
                        onChange={e => handleChange('note', e.target.value)}
                    />
                </Grid>
            </Grid>
            <Button
                sx={{position: "absolute", top: 10, right: 10}}
                size="small"
                variant="outlined"
                color="secondary"
                disabled={disabled}
                onClick={onRemove}
            >
                Odebrat
            </Button>
        </Paper>
    );
};

const GuardiansFields: React.FC<GuardiansFieldsProps> = ({value, onChange, disabled}) => {

    const handleAddGuardian = () => {
        onChange([
            ...value,
            {firstName: '', lastName: '', contact: {email: '', phone: '', note: ''}, note: ''}
        ]);
    };

    const handleRemoveGuardian = (idx: number) => {
        onChange(value.filter((_, i) => i !== idx));
    };

    return (
        <>
            <Typography variant="h6" gutterBottom>Zákonní zástupci</Typography>
            <Stack spacing={2}>
                {(value || []).map((g, idx) => (
                    <GuardianField
                        key={idx}
                        value={g}
                        onChange={updatedG =>
                            onChange(value.map((current, i) => (i === idx ? updatedG : current)))
                        }
                        onRemove={() => handleRemoveGuardian(idx)}
                        disabled={disabled}
                    />
                ))}
                <Button
                    variant="outlined"
                    onClick={handleAddGuardian}
                    disabled={disabled}
                >
                    Přidat zástupce
                </Button>
            </Stack>
        </>
    );
};

export default GuardiansFields;
