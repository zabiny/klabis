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
import {useKlabisApiMutation, useKlabisApiQuery} from "../../api";

interface MemberSuspendConfirmationDialogProps {
    memberId: number;
    open: boolean;
    onClose: () => void;
}

const MemberSuspendConfirmationDialog = ({memberId, open, onClose}: MemberSuspendConfirmationDialogProps) => {
    // Get suspension info
    const {
        data: suspendInfo,
        isLoading: isSuspendInfoLoadingInternal
    } = useKlabisApiQuery("get", "/members/{memberId}/suspendMembershipForm", {params: {path: {memberId: memberId}}});

    // Suspend membership hook
    const suspendMembership = useKlabisApiMutation("put", "/members/{memberId}/suspendMembershipForm");

    const handleSuspendMembership = async () => {
        await suspendMembership.mutateAsync({params: {path: {memberId: memberId}}, body: {}}, {
            onError: () => {
                alert('Failed to suspend membership');
                onClose();
            }, onSettled: () => {
                alert('Membership suspended successfully');
                onClose();
            }
        });
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
                            Stav členství: {suspendInfo.isSuspended ? 'Zrušeno' : 'Aktivní'}
                        </Typography>
                        <Typography variant="body1" gutterBottom>
                            Možnost zrušení: {suspendInfo.canSuspend ? 'Ano' : 'Ne'}
                        </Typography>
                        {suspendInfo.details?.finance && (
                            <Typography variant="body1" gutterBottom>
                                Finanční stav: {suspendInfo.details.finance.status ? 'V pořádku' : 'Nevhodný'}
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
