# À propos de l'application gar-connector
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Régions Ile De France et Nouvelle Aquitaine, Département de Seine et Marne et ville de Paris
* Développeur : CGI, Edifice
* Financeurs : Régions Ile De France et Nouvelle Aquitaine, Département de Seine et Marne et ville de Paris
* Description : Module permettant de se connecter au GAR du Ministère et de réaliser les exports des utilisateurs de la plateforme voulue

# Présentation du module

L'application **gar-connector**, mise à disposition des lycées d'île-de-France, de Nouvelle-Aquitaine et des collèges de la Mairie de Paris et de Seine-et-Marne, permet de gérer :
 - l'accès au GAR Ministériel qui comprend la console d'affectation des ressources et l'accès à la liste des ressources dont l'utilisateur dispose (qui sera consommé et présenté par le module ressource-aggregator https://github.com/OPEN-ENT-NG/ressource-aggregator)
 - les exports des élèves, enseignants et personnels d'un établissement du 1er ou du 2nd degré avec leur classe d'appartenance, modules et fonctions spécifiques. Une interface de paramétrage permet de gérer cela (avec l'ajout/visualisation des responsables d'affectation). Les exports GAR sont réalisés toutes les nuits à partir de 21h et déposés (sous un format tar.gz avec un fichier md5) sur le sftp du GAR Ministériel afin qu'il soit traité et intégré dans leur Base de données
 - le multi-tenant, le module peut gérer l'export de différentes académies sur une seule et unique plateforme

## Configuration
<pre>
{
  "config": {
    ...
    "max-nodes": 10000,
    "export-path": "${garExportPath}data/",
    "export-archive-path": "${garExportPath}data-compress/",
    "export-cron": "${garExportCron}",
    "id-ent" : "${garIdENT}",
    "control-group" : "RESP-AFFECT-GAR",
    "academy-prefix": "${academyPrefix}",
    "gar-ressources" : {
      "host": "${garRessourcesHost}",
      "cert": "${garRessourcesCert}",
      "key": "${garRessourcesKey}"
    },
    "pagination-limit": ${mediacentrePaginationLimit},
    "xsd-recipient-list": ${xsdRecipientList},
    "gar-sftp" : {
      "host": "${garSftpHost}",
      "port": ${garSftpPort},
      "username": "${garSftpUsername}",
      "passphrase": "${garSftpPassphrase}",
      "sshkey": "${garSftpSshkey}",
      "dir-dest": "${garSftpDirDest}",
      "known-hosts": "${garSftpKnownHost}"
    }
  }
}
</pre>

Dans votre springboard, vous devez inclure des variables d'environnement :

<pre>
garExportPath = ${String}
garExportCron = ${String}
garIdENT = ${String}
academyPrefix = ${String}
garRessourcesHost = ${String}
garRessourcesCert = ${String}
garRessourcesKey = ${String}
mediacentrePaginationLimit = Integer
xsdRecipientList = []
garSftpHost = ${String}
garSftpPort = Integer
garSftpUsername = ${String}
garSftpPassphrase = ${String}
garSftpSshkey = ${String}
garSftpDirDest = ${String}
garSftpKnownHost = ${String}
</pre>