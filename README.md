# gar-connector

# À propos de l'application gar-connector
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt)
* Développeur : CGI/ODE
* Financeurs : Régions IDF et CRNA, Département de 77 et ville de Paris
* Description : Module permettant de se connecter au GAR du Ministère et de réaliser les exports des utilisateurs de la plateforme voulue

# Présentation du module

L'application **gar-connector**, mise à disposition des lycées d'île-de-France, de Nouvelle-Aquitaine et des collèges de la Mairie de Paris et de Seine-et-Marne, permet de gérer :
 - l'accès au GAR Ministériel qui comprend la console d'affectation des ressources et l'accès à la liste des ressources dont l'utilisateur dispose (qui sera consommé et présenté par le module ressource-aggregator https://github.com/OPEN-ENT-NG/ressource-aggregator)
 - les exports des élèves, enseignants et personnels d'un établissement du 1er ou du 2nd degré avec leur classe d'appartenance, modules et fonctions spécifiques. Une interface de paramétrage permet de gérer cela (avec l'ajout/visualisation des responsables d'affectation). Les exports GAR sont réalisés toutes les nuits à partir de 21h et déposés (sous un format tar.gz avec un fichier md5) sur le sftp du GAR Ministériel afin qu'il soit traité et intégré dans leur Base de données
 - le multi-tenant, le module peut gérer l'export de différentes académies sur une seule et unique plateforme
