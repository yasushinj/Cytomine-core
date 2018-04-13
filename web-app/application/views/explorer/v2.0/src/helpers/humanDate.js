export default function (date) {
    date = new Date(parseInt(date)).toISOString();
    date = date.replace('T', ' ');
    date = date.substring(0, date.indexOf('.'));
    return date;
}