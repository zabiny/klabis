import {
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Typography,
} from '@mui/material';
import {useGetSuspendMembershipForm, useSuspendMembership} from '../api/membersApi';

interface MemberSuspendConfirmationDialogProps {
    memberId: number;
    open: boolean;
    onClose: () => void;
}

const MemberSuspendConfirmationDialog = ({memberId, open, onClose}: MemberSuspendConfirmationDialogProps) => {
    // Get suspension info
    const {data: suspendInfo, isLoading: isSuspendInfoLoadingInternal} = useGetSuspendMembershipForm(memberId);

    // Suspend membership hook
    const suspendMembership = useSuspendMembership(memberId);

    const handleSuspendMembership = async () => {
        try {
            await suspendMembership.mutateAsync();
            alert('Membership suspended successfully');
            onClose();
        } catch (error) {
            alert('Failed to suspend membership');
            onClose();
        }
    };

    if (isSuspendInfoLoadingInternal) {
        return (
            <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
                <DialogContent>
                    <CircularProgress/>
                </DialogContent>
            </Dialog>
        );
    }

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Zrušení členství</DialogTitle>
            <DialogContent>
                {suspendInfo ? (
                    <Box>
                        <Typography variant="body1" gutterBottom>
                            Jste si jisti, že chcete zrušit členství uživatele?
                        </Typography>
                        <Typography variant="body1" gutterBottom>
                            Stav členství: {suspendInfo.data.isSuspended ? 'Zrušeno' : 'Aktivní'}
                        </Typography>
                        <Typography variant="body1" gutterBottom>
                            Možnost zrušení: {suspendInfo.data.canSuspend ? 'Ano' : 'Ne'}
                        </Typography>
                        {suspendInfo.data.details?.finance && (
                            <Typography variant="body1" gutterBottom>
                                Finanční stav: {suspendInfo.data.details.finance.status ? 'V pořádku' : 'Nevhodný'}
                            </Typography>
                        )}
                    </Box>
                ) : (
                    <Typography variant="body1">
                        Nastala chyba při načítání informací o zrušení členství.
                    </Typography>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} color="primary">
                    Zrušit
                </Button>
                <Button onClick={handleSuspendMembership} variant="contained" color="secondary">
                    Potvrdit
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default MemberSuspendConfirmationDialog;
